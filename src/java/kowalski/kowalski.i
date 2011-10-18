/* 
 * The following is used by SWIG to generate binding source code. 
 */

%module Kowalski
%{
/* Includes the header in the wrapper code */
#include "../../engine/kowalski.h"
%}

/* Parse the header file to generate wrappers */
%include "../../engine/kowalski.h"
