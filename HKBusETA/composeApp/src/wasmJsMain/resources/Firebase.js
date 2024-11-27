const firebaseConfig = {
    apiKey: "AIzaSyA0ewEXdeG98wF1JHnmmY_sXjFQLyn1erg",
    authDomain: "hkbuseta.firebaseapp.com",
    projectId: "hkbuseta",
    storageBucket: "hkbuseta.appspot.com",
    messagingSenderId: "949231290501",
    appId: "1:949231290501:web:d05d962759110d4a8701ec",
    measurementId: "G-Q85F24HQMW"
};
let firebaseAppInstance = null;
getFirebaseApp();

function getFirebaseApp() {
    if (firebaseAppInstance == null) {
        try {
            firebaseAppInstance = firebase.initializeApp(firebaseConfig);
        } catch (err) {
            console.log(err);
        }
    }
    return firebaseAppInstance;
}

function logFirebase(name, keyValues) {
    let parts = keyValues.split('\0');
    let obj = {};
    for (let i = 0; i < parts.length; i += 2) {
        obj[parts[i]] = parts[i + 1];
    }
    let firebaseApp = getFirebaseApp();
    if (firebaseApp != null) {
        firebaseApp.analytics().logEvent(name, obj);
    }
}