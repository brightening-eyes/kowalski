/*
Copyright (c) 2010-2011 Per Gantelius

This software is provided 'as-is', without any express or implied
warranty. In no event will the authors be held liable for any damages
arising from the use of this software.

Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:

   1. The origin of this software must not be misrepresented; you must not
   claim that you wrote the original software. If you use this software
   in a product, an acknowledgment in the product documentation would be
   appreciated but is not required.

   2. Altered source versions must be plainly marked as such, and must not be
   misrepresented as being the original software.

   3. This notice may not be removed or altered from any source
   distribution.
*/

package kowalski.editor;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Event: pitch, gain, readpos, sound/streamingsound
 * retrigger mode (retrigger, fail) (for handles)
 * stealing behaviour (for one shots)^
 * Events can have a sound OR a wave?
 *
 * Group xsd node? group contains group list and list of nodeswith comments and id.
 *
 *
 * TODO:
 * - overwrite prompt when saving/exporting.
 * - recent projects
 * - open->project, wave bank binary, engine data binary
 *
 * - allow any chars in relative paths? windows paths will contain \?
 * - move encoding flag from wavebank to project? or at least duplicate
 * - stereo, positional events...
 * - xml extension to associate files? kwl, kwb
 * - include bundler stub in project.
 * - wave info tool tip
 * - wave dir tree drag and drop
 * - rescan wave dir on regained focus
 * - detect unused wave files^
 * - wave bank wave format
 * - report missing wavbanks when resolving audio data reference.
 * - rearrange engine binary format so event and sound structure match and
 *   so that the parameter set count is explicit per preset.
 * - version and xml file name in binaries
 * - disregard order of events and eventgroups within eventgroups. same for sound. an other elements.
 * - verify that wave paths work on win
 * - check invalid wavebank reference from audio data references
 * - root dir rel to proj file?
 * - kwlAssert with printout.
 * - dependency checking in engine data and wave bank serializers
 * - project tree expansion state. store in data. make node with expansionstate base class in xsd.
 * - custom extension for project data. kwp?
 * - simple example in public api doxygen front page
 */

/**
 * Kowalski editor main class.
 */
public class KowalskiEditor
{
    
    public static void main(String[] args)
    {

        // take the menu bar off the jframe if on os x
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        // set the name of the application menu item if on os x
        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                            "Kowalski Editor");

        try
        {
            //try to switch to native look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (UnsupportedLookAndFeelException e)
        {
            //fail silently
        }
        catch (ClassNotFoundException e)
        {
            //fail silently
        }
        catch (IllegalAccessException e)
        {
            //fail silently
        }
        catch (InstantiationException e)
        {
            //fail silently
        }

        //specify that we want to use custom frame icons instead of the
        //java default one. must be called before any frames are created. 
        JFrame.setDefaultLookAndFeelDecorated(true);

        //fire up the GUI
        KowalskiEditorFrame frame = new KowalskiEditorFrame();
        frame.setSize(800, 600);
        frame.setVisible(true);
    }

}