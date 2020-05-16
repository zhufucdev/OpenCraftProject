module.exports = {
    formatJavaName: (key) => {
        let rename = key.toLowerCase();
        let index;
        while ((index = rename.indexOf('_')) !== -1) {
            const after = rename.substr(index + 1);
            after[0] = after[0].toUpperCase();
            rename = rename.substring(0, index) + after;
        }
        return rename
    },
    toJavaName: (key) => {
        let rename = key;

        function nextUpperCase(from) {
            for (let i = from; i < rename.length; i++) {
                const it = rename[i];
                if (it !== '_' && it.toUpperCase() === it) {
                    return i;
                }
            }
            return -1;
        }

        let index = -1;
        while ((index = nextUpperCase(index + 2)) !== -1) {
            rename = rename.substring(0, index) + "_" + rename.substr(index);
        }
        return rename.toUpperCase();
    }
};
module.shareContext = false;