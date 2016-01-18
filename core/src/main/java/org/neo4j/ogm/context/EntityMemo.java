/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 *  conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.context;

import org.neo4j.ogm.MetaData;
import org.neo4j.ogm.annotations.FieldWriter;
import org.neo4j.ogm.metadata.ClassInfo;
import org.neo4j.ogm.metadata.FieldInfo;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Vince Bickers
 */
public class EntityMemo {

    private final Map<Long, Long> nodeHash = new ConcurrentHashMap<>();
    private final Map<Long, Long> relEntityHash = new ConcurrentHashMap<>();
    private  final MetaData metaData;

    // objects with no properties will always hash to this value.
    private static final long seed = 0xDEADBEEF / (11 * 257);

    public EntityMemo(MetaData meta) {
        metaData = meta;
    }

    /**
     * constructs a 64-bit hash of this object's node properties
     * and maps the object to that hash. The object must not be null
     * @param entityId the id of the entity
     * @param object the object whose persistable properties we want to hash
     * @param classInfo metadata about the object
     */
    public void remember(Long entityId, Object object, ClassInfo classInfo) {
        if (metaData.isRelationshipEntity(classInfo.name())) {
            relEntityHash.put(entityId, hash(object, classInfo));
        }
        else {
            nodeHash.put(entityId, hash(object, classInfo));
        }
    }

    /**
     * determines whether the specified has already
     * been memorised. The object must not be null. An object
     * is regarded as memorised if its hash value in the memo hash
     * is identical to a recalculation of its hash value.
     *
     *
     * @param entityId the id of the entity
     * @param object the object whose persistable properties we want to check
     * @param classInfo metadata about the object
     * @return true if the object hasn't changed since it was remembered, false otherwise
     */
    public boolean remembered(Long entityId, Object object, ClassInfo classInfo) {
        boolean isRelEntity = false;

        if (entityId != null) {
            if (metaData.isRelationshipEntity(classInfo.name())) {
                isRelEntity = true;
            }

            if ((!isRelEntity && !nodeHash.containsKey(entityId)) || (isRelEntity && !relEntityHash.containsKey(entityId))) {
                return false;
            }

            long actual = hash(object, classInfo);
            long expected = isRelEntity ? relEntityHash.get(entityId) : nodeHash.get(entityId);

            return (actual == expected);
        }
        return false;
    }

    public void clear() {
        nodeHash.clear();
        relEntityHash.clear();
    }


    private static long hash(Object object, ClassInfo classInfo) {
        long hash = seed;
        for (FieldInfo fieldInfo : classInfo.propertyFields()) {
            Field field = classInfo.getField(fieldInfo);
            Object value = FieldWriter.read(field, object);
            if (value != null) {
                hash = hash * 31L + hash(value.toString());
            }
        }
        return hash;
    }

    private static long hash(String string) {
        long h = 1125899906842597L; // prime
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = 31*h + string.charAt(i);
        }
        return h;
    }
}
