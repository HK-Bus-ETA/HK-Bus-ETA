isWasmSupported((wasmSupported) => {
    setDownloadAppSheetVisible(isAppleDevice(), isMobileDevice() || !wasmSupported, undefined, wasmSupported);
});