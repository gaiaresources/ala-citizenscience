// Some hacks to fix javascript compatibilities.

// Fix for 'missing' Array.indexOf function. (Basically IE8 and below)
// http://stackoverflow.com/questions/3629183/why-doesnt-indexof-work-on-an-array-ie8
if(!Array.prototype.indexOf) {
    Array.prototype.indexOf = function(elt /*, from*/) {
        var len = this.length >>> 0;
        var from = Number(arguments[1]) || 0;
        from = (from < 0) ? Math.ceil(from) : Math.floor(from);
        if (from < 0) {
            from += len;
        }
        for (; from < len; from++) {
            if (from in this && this[from] === elt) {
                return from;
            }
        }
        return -1;
    };
}

// Fix for 'missing' String.trim function. (First noted on IE8)
// http://stackoverflow.com/questions/2308134/trim-in-javascript-not-working-in-ie
if(typeof String.prototype.trim !== 'function') {
    String.prototype.trim = function() {
        return this.replace(/^\s+|\s+$/g, '');
    }
}

// Patch in the MAX_INT value
if(typeof Number.prototype.MAX_INT !== 'number') {
    // MAX_INT = Math.pow(2, 32) - 1
    Number.prototype.MAX_INT = 4294967295;
    Number.MAX_INT = 4294967295;
}
