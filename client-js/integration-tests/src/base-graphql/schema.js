const { gql } = require('apollo-server-express');



const typeDefs = gql`
    type Query{        
        getItem: Item        
    }
    
    type Item{
        name: String!
    }
`;

module.exports = typeDefs;