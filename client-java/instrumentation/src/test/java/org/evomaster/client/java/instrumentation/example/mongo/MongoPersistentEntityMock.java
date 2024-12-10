package org.evomaster.client.java.instrumentation.example.mongo;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.mapping.*;
import org.springframework.data.mapping.model.PersistentPropertyAccessorFactory;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.core.mapping.ShardKey;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.spel.EvaluationContextProvider;
import org.springframework.data.util.TypeInformation;

import java.lang.annotation.Annotation;
import java.util.Iterator;

public class MongoPersistentEntityMock<T> implements MongoPersistentEntity<T>{

    @Override
    public String getCollection() {
        return null;
    }

    @Override
    public String getLanguage() {
        return null;
    }

    @Override
    public MongoPersistentProperty getTextScoreProperty() {
        return null;
    }

    @Override
    public boolean hasTextScoreProperty() {
        return false;
    }

    @Override
    public Collation getCollation() {
        return null;
    }

    @Override
    public ShardKey getShardKey() {
        return null;
    }

    @Override
    public void addPersistentProperty(MongoPersistentProperty mongoPersistentProperty) {

    }

    @Override
    public void addAssociation(Association<MongoPersistentProperty> association) {

    }

    @Override
    public void verify() throws MappingException {

    }

    @Override
    public void setPersistentPropertyAccessorFactory(PersistentPropertyAccessorFactory persistentPropertyAccessorFactory) {

    }

    @Override
    public void setEvaluationContextProvider(EvaluationContextProvider evaluationContextProvider) {

    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public PreferredConstructor<T, MongoPersistentProperty> getPersistenceConstructor() {
        return null;
    }

    @Override
    public boolean isConstructorArgument(PersistentProperty<?> persistentProperty) {
        return false;
    }

    @Override
    public boolean isIdProperty(PersistentProperty<?> persistentProperty) {
        return false;
    }

    @Override
    public boolean isVersionProperty(PersistentProperty<?> persistentProperty) {
        return false;
    }

    @Override
    public MongoPersistentProperty getIdProperty() {
        return null;
    }

    @Override
    public MongoPersistentProperty getVersionProperty() {
        return null;
    }

    @Override
    public MongoPersistentProperty getPersistentProperty(String s) {
        return null;
    }

    @Override
    public Iterable<MongoPersistentProperty> getPersistentProperties(Class<? extends Annotation> aClass) {
        return null;
    }

    @Override
    public boolean hasIdProperty() {
        return false;
    }

    @Override
    public boolean hasVersionProperty() {
        return false;
    }

    @Override
    public Class<T> getType() {
        return null;
    }

    @Override
    public Alias getTypeAlias() {
        return null;
    }

    @Override
    public TypeInformation<T> getTypeInformation() {
        return null;
    }

    @Override
    public void doWithProperties(PropertyHandler<MongoPersistentProperty> propertyHandler) {

    }

    @Override
    public void doWithProperties(SimplePropertyHandler simplePropertyHandler) {

    }

    @Override
    public void doWithAssociations(AssociationHandler<MongoPersistentProperty> associationHandler) {

    }

    @Override
    public void doWithAssociations(SimpleAssociationHandler simpleAssociationHandler) {

    }

    @Override
    public <A extends Annotation> A findAnnotation(Class<A> aClass) {
        return null;
    }

    @Override
    public <A extends Annotation> boolean isAnnotationPresent(Class<A> aClass) {
        return false;
    }

    @Override
    public <B> PersistentPropertyAccessor<B> getPropertyAccessor(B b) {
        return null;
    }

    @Override
    public <B> PersistentPropertyPathAccessor<B> getPropertyPathAccessor(B b) {
        return null;
    }

    @Override
    public IdentifierAccessor getIdentifierAccessor(Object o) {
        return null;
    }

    @Override
    public boolean isNew(Object o) {
        return false;
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public boolean requiresPropertyPopulation() {
        return false;
    }

    @NotNull
    @Override
    public Iterator<MongoPersistentProperty> iterator() {
        return null;
    }
}