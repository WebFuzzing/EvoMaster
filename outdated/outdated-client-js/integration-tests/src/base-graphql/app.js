const express = require('express');
const { ApolloServer} = require('apollo-server-express');

const typeDefs =  require('./schema');
const resolvers = require('./resolvers');

const app = express();

const apollo = new ApolloServer({ typeDefs, resolvers });
apollo.applyMiddleware({ app , path:"/graphql"});


module.exports = app;