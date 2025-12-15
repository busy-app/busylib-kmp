xcodebuild \
  -scheme "BUSY Lib Sample" \
  -destination "platform=macOS,arch=arm64" \
  -configuration Debug \
  -derivedDataPath ./build-macos \
  build

open ./build-macos/Build/Products/Debug/BUSY\ Lib\ Sample.app