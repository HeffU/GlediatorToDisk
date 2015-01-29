# GlediatorToDisk
A simple tool that modifies the Glediator program so that any ArtNet output is instead saved to files, which can then be moved to other platforms and used as a source for playback.

Leverages AspectJ to modify certain class functionality at load-time, so usage does not modify the original software in any permanent way.

# Usage
Copy the contents of out/artifacts/GlediatorToDisk_jar to the dist folder of a Glediator installation.

Run "run_patched.bat" and configure your project, and Open/Close the ArtNet socket to begin/end recording to file.
Output is labeled by time/date and can be found as .led files in GlediatorToDisk/output/