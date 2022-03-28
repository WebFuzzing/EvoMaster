package com.foo.rest.examples.spring.postcollection;

import com.foo.rest.examples.spring.SpringController;

public class PostCollectionController extends SpringController{

    public PostCollectionController() {
        super(PostCollectionApplication.class);
    }

    @Override
    public void resetStateOfSUT() {
        PostCollectionRest.data.clear();
    }
}
