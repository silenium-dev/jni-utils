#include <jni.h>

extern "C" {
JNIEXPORT jstring JNICALL Java_dev_silenium_libs_jni_TestLibKt_stringFromJNI(JNIEnv *env, jobject thiz) {
	return env->NewStringUTF("jni-string");
}
}
