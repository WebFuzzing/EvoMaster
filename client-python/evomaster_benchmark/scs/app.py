from flask import Flask
from flask_restx import Resource, Api

from evomaster_benchmark.scs.views import calc, cookie, costfuns, date_parse, file_suffix, notypevar, \
                                          ordered4, pat, regex, text2txt, title

app = Flask(__name__)
app.config['RESTX_MASK_SWAGGER'] = False
app.url_map.strict_slashes = False

api = Api(app)
ns = api.namespace('api', description='SCS api')


@ns.route('/calc/<string:op>/<float:arg1>/<float:arg2>')
class Calc(Resource):
    def get(self, op, arg1, arg2):
        return calc(op, arg1, arg2)


@ns.route('/cookie/<string:name>/<string:val>/<string:site>')
class Cookie(Resource):
    def get(self, name, val, site):
        return cookie(name, val, site)


@ns.route('/costfuns/<int:i>/<string:s>')
class Costfuns(Resource):
    def get(self, i, s):
        return costfuns(i, s)


@ns.route('/dateparse/<string:dayname>/<string:monthname>')
class DateParse(Resource):
    def get(self, dayname, monthname):
        return date_parse(dayname, monthname)


@ns.route('/filesuffix/<string:directory>/<string:file>')
class FileSuffix(Resource):
    def get(self, directory, file):
        return file_suffix(directory, file)


@ns.route('/notypevar/<int:i>/<string:s>')
class NotyPevar(Resource):
    def get(self, i, s):
        return notypevar(i, s)


@ns.route('/ordered4/<string:w>/<string:x>/<string:y>/<string:z>')
class Ordered4(Resource):
    def get(self, w, x, y, z):
        return ordered4(w, x, y, z)


@ns.route('/pat/<string:txt>/<string:pattern>')
class Pat(Resource):
    def get(self, txt, pattern):
        return pat(txt, pattern)


@ns.route('/regex/<path:txt>')
class Regex(Resource):
    def get(self, txt):
        return regex(txt)


@ns.route('/text2txt/<string:word1>/<string:word2>/<string:word3>')
class Text2Txt(Resource):
    def get(self, word1, word2, word3):
        return text2txt(word1, word2, word3)


@ns.route('/title/<string:sex>/<string:t>')
class Title(Resource):
    def get(self, sex, t):
        return title(sex, t)


if __name__ == '__main__':
    app.run(debug=True, port=8080)
