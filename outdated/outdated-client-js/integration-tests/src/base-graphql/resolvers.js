
module.exports = {

    Query: {
        getItem: (parent, args, context, info) => {
            return {name: "FOO"};
        }
    }
};