xcodebuild \
  -scheme "BUSY Lib Sample" \
  -destination "platform=iOS Simulator,name=iPhone 17 Pro" \
  -configuration Debug \
  -derivedDataPath ./build-ios \
  build

xcrun simctl boot "iPhone 17 Pro"
xcrun simctl install "iPhone 17 Pro" ./build-ios/Build/Products/Debug-iphonesimulator/BUSY\ Lib\ Sample.app
xcrun simctl launch "iPhone 17 Pro"