isWasmSupported((wasmSupported) => {
    setDownloadAppSheetVisible(isAppleDevice(), (isMobileDevice() && !isStandaloneApp()) || !wasmSupported, undefined, wasmSupported);
});