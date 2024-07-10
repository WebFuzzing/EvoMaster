const Book  = require("./book");

class BookRepository {

    constructor() {
        this.books = new Map();
        this.counter = 0;
    }

    createNewBook(title, author, year) {

        const id = "" + this.counter;
        this.counter++;

        const book = new Book(id, title, author, year);

        this.books.set(id, book);

        return id;
    }

    deleteBook(id) {
        return this.books.delete(id);
    }

    getBook(id) {
        return this.books.get(id);
    }

    getAllBooks(){
        return Array.from(this.books.values());
    }

    updateBook(book) {

        if (! this.books.has(book.id)) {
            return false;
        }

        this.books.set(book.id, book);
        return true;
    }

    getAllBooksSince(year) {
        return Array.from(this.books.values()).filter((b) => b.year >= year);
    }

    reset(){
        this.books.clear();
        this.counter = 0;
    }

}

module.exports = new BookRepository();
