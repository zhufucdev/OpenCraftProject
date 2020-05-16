const object = require('object');
module.exports = function (javaArray) {
    if (!object.isOfClass(javaArray, 'java.lang.Iterable')) {
        throw 'Arugement javaArray must be of {java.lang.Iterable}.'
    }
    let array = [];
    javaArray.forEach(item => array.push(item));
    return array
}
module.shareContext = false;