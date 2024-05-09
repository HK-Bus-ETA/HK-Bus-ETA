isWasmSupported((wasmSupported) => {
    setDownloadAppSheetVisible(isAppleDevice(), isMobileDevice() || !wasmSupported);
});