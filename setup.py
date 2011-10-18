from distutils.core import setup, Extension

module1 = Extension('_Kowalski',
                    include_dirs = ['/usr/local/include'],
                    libraries = ['kowalski', 'portaudio'],
                    library_dirs = ['lib/portaudio/osx', 'dist'],
                    sources = ['src/python/kowalski_wrap.c'],
                    extra_link_args = ['-framework', 'CoreAudio',
                                       '-framework', 'AudioToolbox',
                                       '-framework', 'AudioUnit', 
                                       '-framework', 'CoreServices',
                                       '-v'])

setup (name = 'Kowalski',
       version = '1.0',
       description = 'The Kowalski Engine python bindings.',
       author = '',
       author_email = '',
       url = '',
       long_description = '''
       bla
''',
       ext_modules = [module1])