class BookRepository:
    def __init__(self) -> None:
        self.books = {}
        self.counter = 0

    def create_new_book(self, title: str, author: str, year: int) -> str:
        book_id = self.counter
        self.counter += 1
        book = {
            'book_id': book_id,
            'title': title,
            'author': author,
            'year': year
        }
        self.books[book_id] = book
        return book_id

    def delete_book(self, book_id: int) -> bool:
        if book_id in self.books.keys():
            del self.books[book_id]
            return True
        return False

    def get_book(self, book_id: int):
        return self.books[book_id]

    def get_all_books(self):
        return list(self.books.values())

    def update_book(self, book):
        if book['book_id'] in self.books.keys():
            self.books[book['book_id']] = book
            return True
        return False

    def get_all_books_since(self, year: int):
        return list(filter(lambda b: b.year >= year, self.books.values()))

    def reset(self):
        self.books = {}
        self.counter = 0


repository = BookRepository()
