/*
 * Copyright 2009-2011 Cedric Priscal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * Modifications (2026):
 * - Added missing ioctl/errno includes
 * - Enforced RAW mode and deterministic VMIN/VTIME behavior
 * - Corrected software flow control flags
 * - Added cold-boot robustness: flush + optional DTR/RTS toggle
 * - Improved logging and error checking of TCGETS2/TCSETS2
 *
 * Author: Nicolás Mahnic Garcia <https://github.com/nmahnic>
 */


#include <asm/termios.h>     // termios2 + TCGETS2/TCSETS2 for BOTHER baudrates
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <jni.h>

#include <sys/ioctl.h>       // ioctl()
#include <errno.h>           // errno
#include <android/log.h>

// For TIOCMGET/TIOCMSET and modem-control bits (DTR/RTS).
// On some NDKs, these are available via sys/ioctl.h already, but this helps portability.
#include <linux/tty.h>

#include "SerialPort.h"

static const char *TAG = "serial_port";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

/*
 * Cold-boot reliability helpers
 *
 * SERIALPORT_TOGGLE_DTR_RTS:
 *   When enabled, the code pulses DTR/RTS (LOW -> HIGH) after configuration.
 *   This has been observed to "wake" or "reset" certain UART-attached modules
 *   that remain unresponsive after a full power cycle.
 *
 * If the upstream project prefers to keep behavior unchanged by default,
 * consider setting this to 0 and exposing a Java-side option.
 */
#ifndef SERIALPORT_TOGGLE_DTR_RTS
#define SERIALPORT_TOGGLE_DTR_RTS 1
#endif

#ifndef SERIALPORT_DTR_RTS_LOW_US
#define SERIALPORT_DTR_RTS_LOW_US  (100 * 1000)   // 100 ms low pulse
#endif

#ifndef SERIALPORT_DTR_RTS_HIGH_US
#define SERIALPORT_DTR_RTS_HIGH_US (200 * 1000)   // 200 ms settle time after rising edge
#endif

/*
 * Flush both input and output queues for this file descriptor.
 * This is useful at boot to drop any spurious bytes left by the driver/module.
 */
static void serial_flush_all(int fd) {
    // TCFLSH(TCIOFLUSH) flushes both input and output.
    int r = ioctl(fd, TCFLSH, TCIOFLUSH);
    LOGD("TCFLSH(TCIOFLUSH) r=%d errno=%d (%s)", r, errno, strerror(errno));
}

/*
 * Toggle DTR/RTS low then high to help modules that need an explicit wake/reset pulse.
 * Safe to ignore if the platform does not support it; errors are logged and ignored.
 */
static void serial_toggle_dtr_rts(int fd) {
#if SERIALPORT_TOGGLE_DTR_RTS
    int status = 0;

    int r = ioctl(fd, TIOCMGET, &status);
    LOGD("TIOCMGET r=%d status=0x%x errno=%d (%s)", r, status, errno, strerror(errno));
    if (r != 0) return;

    // Drive DTR/RTS low.
    status &= ~TIOCM_DTR;
    status &= ~TIOCM_RTS;
    r = ioctl(fd, TIOCMSET, &status);
    LOGD("TIOCMSET (LOW) r=%d status=0x%x errno=%d (%s)", r, status, errno, strerror(errno));
    usleep(SERIALPORT_DTR_RTS_LOW_US);

    // Drive DTR/RTS high.
    status |= TIOCM_DTR;
    status |= TIOCM_RTS;
    r = ioctl(fd, TIOCMSET, &status);
    LOGD("TIOCMSET (HIGH) r=%d status=0x%x errno=%d (%s)", r, status, errno, strerror(errno));
    usleep(SERIALPORT_DTR_RTS_HIGH_US);
#else
    (void)fd;
#endif
}

/*
 * Class:     android_serialport_SerialPort
 * Method:    open
 * Signature: (Ljava/lang/String;II)Ljava/io/FileDescriptor;
 */
JNIEXPORT jobject JNICALL Java_android_1serialport_1api_SerialPort_open
        (JNIEnv *env, jclass thiz, jstring path, jint baudrate, jint stopBits, jint dataBits,
                jint parity, jint flowCon, jint flags) {

    (void)thiz;

    int fd;
    jobject mFileDescriptor;

    // ------------------------------------------------------------------------
    // 1) Validate arguments
    // ------------------------------------------------------------------------
    if (baudrate <= 0) {
        LOGE("Invalid baudrate: %d", baudrate);
        return NULL;
    }

    // ------------------------------------------------------------------------
    // 2) Open device node
    // ------------------------------------------------------------------------
    {
        jboolean iscopy;
        const char *path_utf = (*env)->GetStringUTFChars(env, path, &iscopy);

        LOGD("Opening serial port %s with flags 0x%x", path_utf, O_RDWR | flags);
        fd = open(path_utf, O_RDWR | flags);
        LOGD("open() fd = %d", fd);

        (*env)->ReleaseStringUTFChars(env, path, path_utf);

        if (fd == -1) {
            LOGE("Cannot open port errno=%d (%s)", errno, strerror(errno));
            return NULL;
        }
    }

    // ------------------------------------------------------------------------
    // 3) Configure port using termios2 (TCGETS2/TCSETS2)
    // ------------------------------------------------------------------------
    {
        struct termios2 cfg;

        LOGD("Configuring serial port (enter) fd=%d", fd);

        // 3.1 Get current config (must check return value; cfg is undefined on failure).
        int r = ioctl(fd, TCGETS2, &cfg);
        LOGD("TCGETS2 r=%d errno=%d (%s)", r, errno, strerror(errno));
        if (r != 0) {
            LOGE("TCGETS2 failed");
            close(fd);
            return NULL;
        }

        // 3.2 Enforce RAW mode and deterministic behavior
        //
        // Why:
        //  - Avoid canonical mode and line discipline transformations.
        //  - Ensure read() does not block indefinitely (VMIN/VTIME).
        //  - Keep consistent behavior across devices/toolchains.
        cfg.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP | INLCR | IGNCR | ICRNL |
                IXON | IXOFF | IXANY);
        cfg.c_oflag &= ~OPOST;
        cfg.c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);
        cfg.c_cflag |= (CLOCAL | CREAD);

        // Non-blocking-ish read behavior:
        //  VMIN=0, VTIME=5 => read() returns immediately with available bytes,
        //  or returns 0 after ~0.5s if no data arrives.
        cfg.c_cc[VMIN]  = 0;
        cfg.c_cc[VTIME] = 5;

        // 3.3 Set baudrate (termios2 supports arbitrary rates via BOTHER)
        cfg.c_cflag &= ~CBAUD;
        cfg.c_cflag |= BOTHER;
        cfg.c_ispeed = (speed_t)baudrate;
        cfg.c_ospeed = (speed_t)baudrate;

        // 3.4 Data bits
        cfg.c_cflag &= ~CSIZE;
        switch (dataBits) {
            case 5: cfg.c_cflag |= CS5; break;
            case 6: cfg.c_cflag |= CS6; break;
            case 7: cfg.c_cflag |= CS7; break;
            case 8:
            default: cfg.c_cflag |= CS8; break;
        }

        // 3.5 Parity
        switch (parity) {
            case 0: // None
                cfg.c_cflag &= ~PARENB;
                break;
            case 1: // Odd
                cfg.c_cflag |= (PARODD | PARENB);
                cfg.c_iflag &= ~IGNPAR;
                cfg.c_iflag |= (PARMRK | INPCK);
                break;
            case 2: // Even
                cfg.c_iflag &= ~(IGNPAR | PARMRK);
                cfg.c_iflag |= INPCK;
                cfg.c_cflag |= PARENB;
                cfg.c_cflag &= ~PARODD;
                break;
            case 3: // Space
                cfg.c_iflag &= ~IGNPAR;
                cfg.c_iflag |= (PARMRK | INPCK);
                cfg.c_cflag |= (PARENB | CMSPAR);
                cfg.c_cflag &= ~PARODD;
                break;
            case 4: // Mark
                cfg.c_iflag &= ~IGNPAR;
                cfg.c_iflag |= (PARMRK | INPCK);
                cfg.c_cflag |= (PARENB | CMSPAR | PARODD);
                break;
            default:
                cfg.c_cflag &= ~PARENB;
                break;
        }

        // 3.6 Stop bits
        switch (stopBits) {
            case 2: cfg.c_cflag |= CSTOPB; break;
            case 1:
            default: cfg.c_cflag &= ~CSTOPB; break;
        }

        // 3.7 Flow control
        //
        // IMPORTANT FIX:
        //  IXON/IXOFF/IXANY are input flags (c_iflag), not control flags (c_cflag).
        //  Clear both hardware/software flags first to avoid carrying stale state.
        cfg.c_cflag &= ~CRTSCTS;
        cfg.c_iflag &= ~(IXON | IXOFF | IXANY);

        switch (flowCon) {
            case 0: // None
                break;
            case 1: // Hardware RTS/CTS
                cfg.c_cflag |= CRTSCTS;
                break;
            case 2: // Software XON/XOFF
                cfg.c_iflag |= (IXON | IXOFF | IXANY);
                break;
            default:
                break;
        }

        // 3.8 Apply configuration
        r = ioctl(fd, TCSETS2, &cfg);
        LOGD("TCSETS2 r=%d errno=%d (%s)", r, errno, strerror(errno));
        if (r != 0) {
            LOGE("TCSETS2 failed");
            close(fd);
            return NULL;
        }

        LOGD("Configuring serial port (done) fd=%d", fd);

        // --------------------------------------------------------------------
        // 4) Cold-boot robustness (flush + optional DTR/RTS pulse + flush)
        // --------------------------------------------------------------------
        // Rationale:
        //  - Flush any residual bytes before attempting any higher-level protocol.
        //  - Pulse DTR/RTS to wake/reset certain modules after power cycle.
        //  - Flush again in case the module emits spurious bytes on wake.
        serial_flush_all(fd);
        serial_toggle_dtr_rts(fd);
        serial_flush_all(fd);
    }

    // ------------------------------------------------------------------------
    // 5) Build and return java.io.FileDescriptor wrapping the native fd
    // ------------------------------------------------------------------------
    {
        jclass cFileDescriptor = (*env)->FindClass(env, "java/io/FileDescriptor");
        jmethodID iFileDescriptor = (*env)->GetMethodID(env, cFileDescriptor, "<init>", "()V");
        jfieldID descriptorID = (*env)->GetFieldID(env, cFileDescriptor, "descriptor", "I");
        mFileDescriptor = (*env)->NewObject(env, cFileDescriptor, iFileDescriptor);
        (*env)->SetIntField(env, mFileDescriptor, descriptorID, (jint) fd);
    }

    return mFileDescriptor;
}

/*
 * Class:     cedric_serial_SerialPort
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_android_1serialport_1api_SerialPort_close
        (JNIEnv *env, jobject thiz) {
    jclass SerialPortClass = (*env)->GetObjectClass(env, thiz);
    jclass FileDescriptorClass = (*env)->FindClass(env, "java/io/FileDescriptor");

    jfieldID mFdID = (*env)->GetFieldID(env, SerialPortClass, "mFd", "Ljava/io/FileDescriptor;");
    jfieldID descriptorID = (*env)->GetFieldID(env, FileDescriptorClass, "descriptor", "I");

    jobject mFd = (*env)->GetObjectField(env, thiz, mFdID);
    jint descriptor = (*env)->GetIntField(env, mFd, descriptorID);

    LOGD("close(fd = %d)", descriptor);
    close(descriptor);
}

