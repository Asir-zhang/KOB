export class Cell { //蛇身体的一小格
    constructor(r, c){
        this.r = r;
        this.c = c;
        this.x = c + 0.5;
        this.y = r + 0.5;
    }
}
