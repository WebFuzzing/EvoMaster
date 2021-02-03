from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()

class News(db.Model):
    __tablename__ = "news"
    id = db.Column(db.Integer, primary_key=True)
    authorId = db.Column(db.String(32))
    text = db.Column(db.String(1024))
    creationTime = db.Column(db.String(1024))  # todo: use datetime
    country = db.Column(db.String())

    def __repr__(self):
        return '<News %s>' % self.title

    def save_to_db(self) -> None:
        db.session.add(self)
        db.session.commit()

    def delete_from_db(self) -> None:
        db.session.delete(self)
        db.session.commit()

    def update(self, data) -> None:
        self.authorId = data['authorId']
        self.text = data['text']
        self.creationTime = data['creationTime']
        self.country = data['country']
        db.session.commit()

    def update_text(self, text) -> None:
        self.text = text
        db.session.commit()
