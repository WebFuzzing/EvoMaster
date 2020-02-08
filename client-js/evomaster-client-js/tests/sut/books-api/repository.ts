import Book from "./book";

class BookRepository {

    private readonly books = new Map<string, Book>();

    private counter: number = 0;

    public createNewBook(title: string, author: string, year: number): string {

        const id = "" + this.counter;
        this.counter++;

        const book = new Book(id, title, author, year);

        this.books.set(id, book);

        return id;
    }

    public deleteBook(id: string): boolean {
        return this.books.delete(id);
    }

    public getBook(id: string): Book {
        return this.books.get(id);
    }

    public getAllBooks(): Book[] {
        return Array.from(this.books.values());
    }

    public updateBook(book: Book) {

        if (! this.books.has(book.id)) {
            return false;
        }

        this.books.set(book.id, book);
        return true;
    }

    public getAllBooksSince(year: number): Book[] {
        return Array.from(this.books.values()).filter((b) => b.year >= year);
    }

    public reset(): void {
        this.books.clear();
        this.counter = 0;
    }

}

export default new BookRepository();
