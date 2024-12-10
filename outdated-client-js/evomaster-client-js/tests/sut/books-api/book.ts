
export default class Book {

    public id: string;
    public title: string;
    public author: string;
    public year: number;

    constructor(id: string, title: string, author: string, year: number) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.year = year;
    }
}
