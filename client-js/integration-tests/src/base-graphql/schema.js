const { gql } = require('apollo-server-express');



const typeDefs = gql`
    type Query{        
        getItem: Item        
    }
    
    type Item{
        id: String!
    }
`;

module.exports = typeDefs;