mkdir temp
cp -r "dist/Kowalski Demo.app" temp
cp license.txt temp/license.txt
rm "dist/Kowalski Demo.dmg"
hdiutil create -fs HFS+ -volname "Kowalski Demo" -srcfolder \
"temp" "dist/Kowalski Demo.dmg"
rm -rf temp
