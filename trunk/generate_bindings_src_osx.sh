#Invokes SWIG to create source code for the Kowalski bindings. 
#This script only generates code and does not do any compilation.

cd tools/swig/osx

#java bindings
echo Generating java bindings code
swig -java -package kowalski -o ../../../src/java/kowalski/kowalski_wrap.c -outdir ../../../src/java/kowalski ../../../src/java/kowalski/kowalski.i

#python bindings
echo Generating python bindings code
swig -python -o ../../../src/python/kowalski_wrap.c -outdir ../../../src/python ../../../src/java/kowalski/kowalski.i

cd ../../..
