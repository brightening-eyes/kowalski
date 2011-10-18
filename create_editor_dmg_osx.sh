mkdir temp
cp -r "dist/Kowalski Editor.app" temp
cp license.txt temp/license.txt
rm "dist/Kowalski Editor.dmg"
hdiutil create -fs HFS+ -volname "Kowalski Editor" -srcfolder \
"temp" "dist/Kowalski Editor.dmg"
rm -rf temp
