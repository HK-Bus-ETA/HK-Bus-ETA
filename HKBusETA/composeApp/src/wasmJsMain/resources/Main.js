function isWasmSupported(callback) {
    try {
        if (typeof WebAssembly === "object"  && typeof WebAssembly.instantiate === "function") {
            const module = new WebAssembly.Module(Uint8Array.of(0x0, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00));
            if (module instanceof WebAssembly.Module) {
                if (new WebAssembly.Instance(module) instanceof WebAssembly.Instance) {
                    wasmFeatureDetect.gc().then(callback);
                    return;
                }
            }
        }
    } catch (e) {
    }
    callback(false);
}

function isDownloadAppSheetVisible() {
    return document.getElementById("download-app").classList.contains("modal-bottom-sheet-shown");
}

function setDownloadAppSheetVisible(isApple, visible, forceDarkMode, wasmSupported) {
    let darkMode = forceDarkMode
    if (darkMode == undefined && window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
        darkMode = true
    }
    if (darkMode) {
        document.getElementById("download-app").classList.add("dark-modal-bottom-sheet");
    } else if (darkMode != undefined) {
        document.getElementById("download-app").classList.remove("dark-modal-bottom-sheet");
    }
    if (isApple) {
        document.getElementById("store-url").href = "https://apps.apple.com/app/id6475241017";
        document.getElementById("store-alt").href = "https://play.google.com/store/apps/details?id=com.loohp.hkbuseta";
        document.getElementById("store-banner").src = "appstore.png";
        document.getElementById("store-banner").alt = "在App Store下載 Download on the App Store";
        document.getElementById("store-alt").innerHTML = "或在Google Play下載 Or download on Google Play";
    } else {
        document.getElementById("store-url").href = "https://play.google.com/store/apps/details?id=com.loohp.hkbuseta";
        document.getElementById("store-alt").href = "https://apps.apple.com/app/id6475241017";
        document.getElementById("store-banner").src = "googleplay.png";
        document.getElementById("store-banner").alt = "在Google Play下載 Download on Google Play";
        document.getElementById("store-alt").innerHTML = "或在App Store下載 Or download on the App Store";
    }
    if (wasmSupported === true) {
        document.getElementById("continue-button").innerHTML = "繼續使用瀏覽器<br>Continue in Browser";
        document.getElementById("continue-button").disabled = false;
    } else if (wasmSupported === false) {
        document.getElementById("continue-button").innerHTML = "您的瀏覽器不支援WASM<br>Your browser does not support WASM";
        document.getElementById("continue-button").disabled = true;
    } else {
        document.getElementById("continue-button").innerHTML = "正在檢查您的瀏覽器<br>Checking your Browser";
        document.getElementById("continue-button").disabled = true;
    }
    if (visible) {
        document.getElementById("download-app-backdrop").classList.add("modal-backdrop-shown");
        document.getElementById("download-app-backdrop").style.display = "";
        document.getElementById("download-app").classList.add("modal-bottom-sheet-shown");
        document.getElementById("download-app").style.display = "";
    } else {
        document.getElementById("download-app-backdrop").classList.remove("modal-backdrop-shown");
        setTimeout(() => document.getElementById("download-app-backdrop").style.display = "none", 200);
        document.getElementById("download-app").classList.remove("modal-bottom-sheet-shown");
        setTimeout(() => document.getElementById("download-app").style.display = "none", 200);
    }
}

function isAppleDevice() {
    return /iPad|iPhone|iPod|Macintosh/.test(navigator.platform) || navigator.platform === 'MacIntel';
}

function isMobileDevice() {
    return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
}

function shareUrlMenu(url, title) {
    if (navigator.share) {
        navigator.share({title: title, url: url});
        return true;
    }
    return false;
}

async function decompressBase64GzipToBase64(inputBase64) {
    const binaryString = window.atob(inputBase64);
    const len = binaryString.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) {
        bytes[i] = binaryString.charCodeAt(i);
    }
    const blob = new Blob([bytes], { type: 'application/gzip' });
    const ds = new DecompressionStream('gzip');
    const decompressedStream = blob.stream().pipeThrough(ds);
    return new Response(decompressedStream).arrayBuffer().then((arrayBuffer) => {
        const uintArray = new Uint8Array(arrayBuffer);
        let resultBase64 = '';
        const chunkSize = 0x8000;
        let c = 0;
        for (let i = 0; i < uintArray.length; i += chunkSize) {
            c = uintArray.subarray(i, i + chunkSize);
            resultBase64 += String.fromCharCode.apply(null, c);
        }
        return window.btoa(resultBase64);
    });
}

function canDecodeGzip() {
    return 'DecompressionStream' in window
}

function decodeGzip(data, callback) {
    decompressBase64GzipToBase64(data).then((plain) => {
        callback(plain);
    });
}

function readFromIndexedDB(key, callback) {
    const dbName = "Database";
    const storeName = "Files";
    const openRequest = indexedDB.open(dbName, 2);
    openRequest.onupgradeneeded = function(event) {
        const db = event.target.result;
        if (!db.objectStoreNames.contains(storeName)) {
            db.createObjectStore(storeName);
        }
    };
    openRequest.onsuccess = function(event) {
        const db = event.target.result;
        const transaction = db.transaction(storeName, 'readonly');
        const store = transaction.objectStore(storeName);
        const request = store.get(key);
        request.onsuccess = function() {
            callback(request.result);
        };
        request.onerror = function() {
            console.error('Error reading data from IndexedDB', request.error);
        };
    };
    openRequest.onerror = function(event) {
        console.error('Error opening IndexedDB', event.target.errorCode);
    };
}

function writeToIndexedDB(key, data) {
    const dbName = "Database";
    const storeName = "Files";
    const openRequest = indexedDB.open(dbName, 2);
    openRequest.onupgradeneeded = function(event) {
        const db = event.target.result;
        if (!db.objectStoreNames.contains(storeName)) {
            db.createObjectStore(storeName);
        }
    };
    openRequest.onsuccess = function(event) {
        const db = event.target.result;
        const transaction = db.transaction(storeName, 'readwrite');
        const store = transaction.objectStore(storeName);
        const request = store.put(data, key);
        request.onsuccess = function() {
            //success
        };
        request.onerror = function() {
            console.error('Error writing data to IndexedDB', request.error);
        };
    };
    openRequest.onerror = function(event) {
        console.error('Error opening IndexedDB', event.target.errorCode);
    };
}

function listAllKeysInIndexedDB(callback) {
    const dbName = "Database";
    const storeName = "Files";
    const openRequest = indexedDB.open(dbName, 2);
    openRequest.onupgradeneeded = function(event) {
        const db = event.target.result;
        if (!db.objectStoreNames.contains(storeName)) {
            db.createObjectStore(storeName);
        }
    };
    openRequest.onsuccess = function(event) {
        const db = event.target.result;
        const transaction = db.transaction(storeName, 'readonly');
        const store = transaction.objectStore(storeName);
        const request = store.getAllKeys();
        request.onsuccess = function() {
            callback(request.result.join('\0'));
        };
        request.onerror = function() {
            console.error('Error listing keys from IndexedDB', request.error);
        };
    };
    openRequest.onerror = function(event) {
        console.error('Error opening IndexedDB', event.target.errorCode);
    };
}

function deleteFromIndexedDB(key, callback) {
    const dbName = "Database";
    const storeName = "Files";
    const openRequest = indexedDB.open(dbName, 2);
    openRequest.onupgradeneeded = function(event) {
        const db = event.target.result;
        if (!db.objectStoreNames.contains(storeName)) {
            db.createObjectStore(storeName);
        }
    };
    openRequest.onsuccess = function(event) {
        const db = event.target.result;
        const transaction = db.transaction(storeName, 'readwrite');
        const store = transaction.objectStore(storeName);
        const deleteRequest = store.delete(key);
        deleteRequest.onsuccess = function() {
            callback(true);
        };
        deleteRequest.onerror = function() {
            callback(false);
        };
    };
    openRequest.onerror = function(event) {
        console.error('Error opening IndexedDB', event.target.errorCode);
    };
}

function readFile(callback) {
    var input = document.createElement('input');
    input.type = 'file';
    input.onchange = e => {
       var file = e.target.files[0];
       var reader = new FileReader();
       reader.readAsText(file,'UTF-8');
       reader.onload = readerEvent => {
          callback(readerEvent.target.result);
       }
    }
    input.click();
}

function writeFile(fileName, fileContent) {
    var blob = new Blob([fileContent], {type: "text/plain;charset=utf-8"});
    saveAs(blob, fileName);
}

function getLocation(position, error) {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition((result) => {
            position(result.coords.latitude, result.coords.longitude);
        }, (reason) => {
            switch(error.code) {
                case error.PERMISSION_DENIED:
                    error(true);
                    break;
                case error.POSITION_UNAVAILABLE:
                    error(false);
                    break;
                case error.TIMEOUT:
                    error(false);
                    break;
                case error.UNKNOWN_ERROR:
                    error(false);
                    break;
                default:
                    error(false);
                    break;
            }
        });
    } else {
        error(false);
    }
}