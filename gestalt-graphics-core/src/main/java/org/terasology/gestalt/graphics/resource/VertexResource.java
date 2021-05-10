// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.graphics.resource;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

public class VertexResource {
    private int inStride = 0;
    private int inSize = 0;
    private int version = 0;
    private ByteBuffer buffer = BufferUtils.createByteBuffer(0);
    private VertexDefinition[] attributes;

    public int inSize() {
        return inSize;
    }

    public VertexResource() {

    }

    public VertexDefinition[] definitions() {
        return this.attributes;
    }

    public ByteBuffer buffer() {
        return this.buffer;
    }

    public void setDefinitions(VertexDefinition[] attr) {
        this.attributes = attr;
    }

    public VertexResource(int inSize, int inStride, VertexDefinition[] attributes) {
        this.inStride = inStride;
        this.inSize = inSize;
        this.attributes = attributes;
        this.buffer = BufferUtils.createByteBuffer(inSize);
    }

    public void copy(VertexResource resource) {
        if (resource.inSize == 0) {
            return;
        }
        ensureCapacity(resource.inSize);
        ByteBuffer copyBuffer = resource.buffer;
        copyBuffer.limit(resource.inSize());
        copyBuffer.rewind();
        buffer.put(copyBuffer);

        this.inSize = resource.inSize;
        this.inStride = resource.inStride;
        this.mark();
    }

    public int getInSize() {
        return inSize;
    }

    public int getInStride() {
        return inStride;
    }

    public void ensureCapacity(int size) {
        if (size > buffer.capacity()) {
            int newCap = Math.max(this.inSize << 1, size);
            ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCap);
            buffer.limit(Math.min(size, this.inSize));
            buffer.position(0);
            newBuffer.put(buffer);
            this.buffer = newBuffer;
        }
        if (size > this.inSize) {
            this.inSize = size;
        }
    }

    public void reserveElements(int verts) {
        int size = verts * inStride;
        ensureCapacity(size);
    }

    public void  reallocateElements(int verts) {
        int size = verts * inStride;
        ensureCapacity(size);
        this.inSize = size;
        squeeze();

    }

    public void squeeze() {
        if (this.inSize != buffer.capacity()) {
            ByteBuffer newBuffer = BufferUtils.createByteBuffer(this.inSize);
            buffer.limit(this.inSize);
            buffer.position(0);
            newBuffer.put(buffer);
            this.buffer = newBuffer;
        }
    }

    public void allocate(int size, int stride) {
        this.ensureCapacity(size);
        this.inStride = stride;
        this.inSize = size;
    }

    public int getVersion() {
        return version;
    }

    /**
     * increase version flag for change
     */
    public void mark() {
        version++;
    }

    /**
     * describes the metadata and placement into the buffer based off the stride.
     */
    public static class VertexDefinition {
        public final int location;
        public final VertexAttribute attribute;
        public final int offset;

        public VertexDefinition(int location, int offset, VertexAttribute attribute) {
            this.location = location;
            this.attribute = attribute;
            this.offset = offset;
        }
    }
}
