#compiles the c glue code generated by SWIG into a dynamic library that can be loaded from java code.
#the produces library is a fat binary (ppc, i386 and x86_64)

#gcc -dynamiclib -c -v -fPIC kowalski_wrap.c -I/Library/Java/Home/include
#libtool -v -dynamic -Ldist -Llib/portaudio/osx -lkowalski_osx -lportaudio -framework CoreAudio -framework AudioToolbox -framework AudioUnit -framework CoreServices -o kowalski_osx.jnilib
gcc -dynamiclib -arch x86_64 -arch ppc -arch i386 -v -fPIC src/java/kowalski/kowalski_wrap.c -I/Library/Java/Home/include -Ldist -Llib/portaudio/osx -lkowalski -lportaudio -framework CoreAudio -framework AudioToolbox -framework AudioUnit -framework CoreServices -o dist/kowalski_java.jnilib
