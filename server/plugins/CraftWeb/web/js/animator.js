function startAnimation(handler,from,to,duration,period,onDone) {
    if (period === undefined) 
        period = 10;
    else if (typeof period === 'function'){
        onDone = period;
        period = 10;
    }
    if (duration === undefined) throw 'Duration must be defined.';
    let x = -Math.PI/2, calc;
    let times = duration / period;
    let once = Math.PI / times;
    let subtract = Math.abs(from - to);
    if (from > to) {
        calc = () => {
            x += once;
            return from - subtract * (Math.sin(x) + 1) / 2;
        }
    } else {
        calc = () => {
            x -= once;
            return from + subtract * (Math.sin(x) + 1) / 2;
        }
    }
    let s = 0;
    let interval = setInterval(() => {
        s++;
        if (s >= times) {
            handler(to);
            clearInterval(interval);
            if (typeof onDone === 'function') 
                onDone()
        } else handler(calc());
    }, period)
}