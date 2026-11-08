.PHONY: build install dev-install clean stop prod prod-install emulator test test-unit check screenshots

# Default task: Builds the debug APK
build:
	@echo "Building Debug APK..."
	./gradlew assembleDebug

prod:
	@echo "Building Production APK..."
	./gradlew assembleRelease

# Waits for a connected device or running emulator
emulator:
	@if ! adb devices | grep -wq "device"; then \
		echo "No device detected. Waiting for connected device or emulator..."; \
		adb wait-for-device; \
		echo "Waiting for boot to complete..."; \
		until [ "$$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do \
			sleep 2; \
		done; \
		echo "Device ready."; \
	else \
		echo "Device/Emulator already running."; \
	fi

# Builds and installs the built APK to a connected device or emulator
dev-install: emulator
	@echo "Building and installing to device..."
	./gradlew installDebug

# Installs the production APK to a connected device or emulator
install: emulator
	@echo "Installing Production APK..."
	./gradlew installRelease

# Alias for installing production APK
prod-install: install

# Cleans the build directory to save disk space
clean:
	@echo "Cleaning build artifacts..."
	./gradlew clean

stop:
	@echo "Stopping gradle daemon..."
	./gradlew --stop

# Runs fast local unit, integration & UI tests on JVM
test-unit:
	@echo "Running local unit & integration tests..."
	./gradlew testDebugUnitTest

# Runs instrumented tests on connected device/emulator
test:
	@echo "Testing..."
	./gradlew connectedAndroidTest --rerun-tasks

# Full pre-commit gate: lint + unit tests
check:
	@echo "Running lint and unit tests..."
	./gradlew lintDebug testDebugUnitTest

# Captures README screenshots at 1080x2400 in dark mode with Material You colors.
# Needs one running Android 12+ emulator; set ANDROID_SERIAL when several devices
# are attached. Uses a temporary emulator user so existing app data stays untouched.
screenshots:
	@echo "Capturing screenshots..."
	./scripts/screenshots.sh
