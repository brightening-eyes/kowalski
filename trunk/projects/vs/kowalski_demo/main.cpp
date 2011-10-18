// kowalski_demo.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"

#include <SDL/SDL.h>
#include "KowalskiDemoManager.h"

int _tmain(int argc, _TCHAR* argv[])
{
	//create the demo manager and fire up its main loop
    KowalskiDemoManager* demo = new KowalskiDemoManager();
    demo->mainLoop();
    
    //clean up
    delete demo;
    return 0;
}

