function draw(canvas,yray,xray,drawLine) {
    if (canvas === undefined) return;
    let ctx = canvas.getContext('2d');
    
    if (xray.length <= 0) {
        ctx.fillStyle = 'black';
        ctx.font = '32px arial';
        let text = '无记录';
        ctx.width = canvas.width;ctx.height = canvas.height;
        ctx.fillText(text,(ctx.width - ctx.measureText(text).width)/2,ctx.height/2);
        return
    }
    
    let max = 0;

    for (let i = 0; i < yray.length; i++) {
        if (yray[i] > max) max = yray[i]
    }

    let xOffset = ctx.measureText(max.toString()).width + 10;
    let width = canvas.width = (yray.length) * 50 + xOffset;
    let height = canvas.height;
    let yRadio = (height - 100) / max;

    ctx.fillStyle = '#FF7043';
    ctx.fillRect(xOffset, 50, 5, height - 100);

    ctx.strokeStyle = 'black';
    ctx.font = '12px arial';
    for (let y = 0; y <= height - 100; y += 50) {
        let value = (y / yRadio).toFixed(0);
        ctx.strokeText(value, xOffset - ctx.measureText(value).width - 5, height - 50 - y)
    }
    let index = 0;
    if (drawLine === undefined) drawLine = false;

    ctx.shadowOffsetX = ctx.shadowOffsetY = 1;
    ctx.shadowBlur = 6;
    ctx.shadowColor = 'rgba(0,0,0,0.5)';
    ctx.strokeStyle = ctx.fillStyle;
    for (let x = 0; x < yray.length; x++) {
        let X = x * 50 + xOffset + 15;
        let random = yray[index];
        let h = height - yRadio * random - 50;
        if (drawLine) {
            //折线
            ctx.lineTo(X + 10, h);
            ctx.stroke();

            ctx.beginPath();
            ctx.moveTo(X + 10, h);
        } else {
            //条形
            ctx.fillRect(X, h, 20, height - 50 - h);
        }

        index++;
    }
    index = 0;
    ctx.shadowColor = 'rgba(0,0,0,0.3)';
    ctx.shadowBlur = 2;
    ctx.strokeStyle = 'black';
    for (let x = 0; x < xray.length; x++) {
        let X = x * 50 + xOffset + 15;
        let text = xray[x];
        ctx.strokeText(text, X + (20 - ctx.measureText(text).width)/2, height - 30);
        let random = yray[index];
        let h = height - yRadio * random - 50;

        if (drawLine) {
            ctx.beginPath();
            ctx.arc(X + 10,h,5,0,Math.PI*2);
            ctx.fill();
        }

        let numberXOffset = 0, numberHOffset = -5;
        let previous = yray[index - 1], next = yray[index + 1];
        if (drawLine) {
            if (previous !== undefined && next !== undefined) {
                numberXOffset = previous === random && next === random ? 0
                    : (
                        previous > random && next > random ? -15
                            : previous >= random && next <= random ? 15
                            : previous < random && next > random ? -15
                                : 0
                    );
                numberHOffset = previous === random && next === random ? -15
                    : (
                        previous >= random && next >= random ? h + 15 < height - 50 ? 15 : -5
                            : previous < random && next < random ? -15
                            : 0
                    )
            }
        }
        ctx.fillText(
            random.toString(),
            X + (20 - ctx.measureText(random.toString()).width)/2 + numberXOffset,
            h + numberHOffset
        );

        index ++;
    }

    ctx.fillRect(xOffset, height - 50, width, 5);
}

function clear(ele) {
    let ctx = ele.getContext('2d');
    ctx.width = ele.width;ctx.height = ele.height;
    ctx.fillStyle = 'white';
    ctx.fillRect(0,0,ctx.width,ctx.height)
}