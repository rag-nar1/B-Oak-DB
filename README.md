# Disk Manager - B+ Tree Storage Engine

A high-performance, concurrent disk-based storage engine written in Java, implementing a B+ tree indexing structure with sophisticated buffer pool management and asynchronous disk I/O operations.

## Project Overview

This project is a complete database storage engine that provides:
- **B+ Tree Indexing**: Efficient data storage and retrieval with logarithmic time complexity
- **Buffer Pool Management**: Intelligent memory management with LRU eviction policies
- **Concurrent Access Control**: Thread-safe operations with fine-grained locking
- **Asynchronous Disk I/O**: Non-blocking disk operations for improved performance
- **Type System**: Support for multiple data types with efficient serialization

## Architecture Overview

The system follows a layered architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
├─────────────────────────────────────────────────────────────┤
│                   Index Manager                             │
│              (Collection Management)                        │
├─────────────────────────────────────────────────────────────┤
│                     B+ Tree                                 │
│           (Indexing & Query Processing)                     │
├─────────────────────────────────────────────────────────────┤
│                   Buffer Pool                               │
│              (Memory Management)                            │
├─────────────────────────────────────────────────────────────┤
│                  Disk Manager                               │
│              (Persistent Storage)                           │
└─────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. Disk Manager (`src/diskmanager/`)

**Purpose**: Handles all disk I/O operations with asynchronous request processing.

**Key Features**:
- **Asynchronous I/O**: Uses a blocking queue and thread pool for non-blocking disk operations
- **File Management**: Automatic file creation and management in the `storage/` directory
- **Page Allocation**: Pre-allocates pages in chunks (1024 pages = 8MB) for better performance
- **Concurrent Access**: Thread-safe operations with per-file resize locks

**Key Classes**:
- `BasicDiskManager`: Main implementation with request queue processing
- `DiskRequest`: Encapsulates read/write requests with completion futures
- `RandomAccessDiskFile`: File abstraction for page-based I/O

### 2. Buffer Pool (`src/bufferpool/`)

**Purpose**: Manages memory efficiently with caching and eviction policies.

**Key Features**:
- **LRU Eviction**: Least Recently Used replacement policy with configurable K parameter
- **Pin/Unpin Mechanism**: Pages can be pinned to prevent eviction during operations
- **Guard System**: Read/Write guards provide safe concurrent access to pages
- **Dirty Page Management**: Automatic flushing of modified pages to disk

**Key Classes**:
- `BufferPool`: Main buffer pool implementation
- `Frame`: Represents a page in memory with metadata
- `LRU`: Implements the LRU replacement algorithm
- `ReadGuard`/`WriteGuard`: Provide safe concurrent access to pages

### 3. B+ Tree (`src/btree/`)

**Purpose**: Primary indexing structure providing efficient data storage and retrieval.

**Key Features**:
- **Optimistic Concurrency**: Uses optimistic locking for better performance
- **Node Splitting**: Automatic node splitting when capacity is exceeded
- **Range Queries**: Support for cursor-based range scanning
- **Composite Keys**: Support for multi-column keys through `Compositekey`

**Key Classes**:
- `Btree`: Main B+ tree implementation with insert/search/delete operations
- `BtreeHeader`: Manages tree metadata (root page ID, height)
- `Cursor`: Provides iterator-style access for range queries

### 4. Page Management (`src/page/`)

**Purpose**: Manages the structure of internal and leaf nodes in the B+ tree.

**Key Features**:
- **Node Types**: Separate implementations for internal and leaf nodes
- **Variable-Length Records**: Efficient storage of variable-sized data
- **Split Operations**: Sophisticated node splitting with sibling redistribution
- **Linked Leaf Nodes**: Leaf nodes are linked for efficient range queries

**Key Classes**:
- `LeafNode`: Stores actual key-value pairs
- `InternalNode`: Stores routing information for tree navigation
- `TreeNodeHeader`: Common header structure for all node types

### 5. Type System (`src/types/`)

**Purpose**: Provides efficient serialization and comparison for various data types.

**Key Features**:
- **Primitive Types**: Support for Integer, Long, Double, Float, Short, Byte
- **Composite Keys**: Multi-column keys with proper comparison semantics
- **Memory Codecs**: Efficient binary serialization/deserialization
- **Type Safety**: Compile-time type checking with generic templates

**Key Classes**:
- `Compositekey`: Multi-column key implementation
- `Template`: Type definition for keys and values
- `Key`: Individual key component with type-specific codecs

### 6. Index Manager (`src/indexmanager/`)

**Purpose**: Provides higher-level index management and collection support.

**Key Features**:
- **Collection Management**: Organizes indexes by collection name
- **Index Naming**: Uses "collectionName-fieldName" naming convention
- **Lifecycle Management**: Handles index creation and cleanup

## Configuration

Key system parameters are defined in `src/globals/Globals.java`:

```java
public static final int PAGE_SIZE = 2 * 4096;              // 8KB pages
public static final int CLUSTER_PAGE_SIZE = 4 * 4096;      // 16KB clusters  
public static final int PRE_ALLOCATED_PAGES_COUNT = 1024;  // 8MB pre-allocation
public static final long INVALID_PAGE_ID = -1;             // Invalid page marker
```

## Performance Characteristics

- **Time Complexity**: O(log n) for search, insert, and delete operations
- **Space Complexity**: Configurable buffer pool size with LRU eviction
- **Concurrency**: Optimistic locking with fine-grained page-level locks
- **I/O Efficiency**: Page-based storage with asynchronous disk operations

## Usage Example

```java
// Initialize components
DiskManager diskManager = new BasicDiskManager();
BufferPool bufferPool = new BufferPool(4000, 10, diskManager);

// Create B+ tree
Template keyType = new Template(Integer.class);
Template valueType = new Template(String.class);
Btree btree = new Btree(keyType, valueType, "myindex", 
                       Globals.INVALID_PAGE_ID, bufferPool);

// Insert data
Compositekey key = new Compositekey(keyType);
key.set(0, 42, Integer.class);
Compositekey value = new Compositekey(valueType);
value.set(0, "Hello World", String.class);
btree.insert(key, value);

// Search data
Compositekey result = btree.get(key);
```

## Build and Run

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher

### Building
```bash
# Compile all source files
javac -d bin $(find src -name "*.java")

# Or use Maven
mvn compile
```

### Running Tests
```bash
# Run with Maven
mvn test

# Or run manually
java -cp bin org.junit.runner.JUnitCore test.btree.BtreeTest
```

### Running the Application
```bash
java -cp bin Main
```

## Storage Layout

The system stores data in the `storage/` directory with the following structure:
- Each index is stored as a separate file
- Files use page-based layout (8KB pages)
- Header pages contain metadata (root page ID, tree height)
- Data pages contain either internal nodes or leaf nodes

## Thread Safety

The system is designed for high concurrency:
- **Buffer Pool**: Uses fine-grained locking with per-frame locks
- **Disk Manager**: Asynchronous request processing with thread-safe queues
- **B+ Tree**: Optimistic concurrency control with context-based locking

## Testing

Comprehensive test suites are provided:
- **Unit Tests**: Individual component testing
- **Integration Tests**: Cross-component functionality
- **Performance Tests**: Benchmarking with up to 1M operations
- **Concurrency Tests**: Multi-threaded stress testing

## Future Enhancements

Potential areas for improvement:
- **Compression**: Page-level compression for better storage efficiency
- **Logging**: Write-ahead logging for crash recovery
- **Clustering**: Support for clustered indexes
- **String Types**: Enhanced support for variable-length strings
- **Transactions**: ACID transaction support

## License

This project is licensed under the terms specified in the LICENSE file.