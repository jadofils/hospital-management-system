// MongoDB init script for notifications collection
// Run in the mongo shell or via `docker exec` into the mongo container.

db = db.getSiblingDB('hospital');

// Create collection with sensible defaults
db.createCollection('notifications', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['type','recipients','createdAt'],
      properties: {
        type: { bsonType: 'string' },
        actorUserId: { bsonType: 'string' },
        recipients: { bsonType: 'array', items: { bsonType: 'string' } },
        payload: { bsonType: ['object','null'] },
        channels: { bsonType: ['array','null'] },
        status: { bsonType: ['object','null'] },
        createdAt: { bsonType: 'date' },
        priority: { enum: ['low','normal','high'], default: 'normal' }
      }
    }
  }
});

// Index: fast per-user reads (recipient lookup + recent)
db.notifications.createIndex({ 'recipients': 1, createdAt: -1 }, { name: 'idx_recipient_createdAt' });

// Index: resource queries
db.notifications.createIndex({ 'resource.type': 1, 'resource.id': 1 }, { name: 'idx_resource' });

// Partial index: unread for users (efficient unread counts)
// Note: Mongo partial index can't easily index dynamic keys for status.{userId}.read;
// as an alternative, include an explicit `unreadRecipients` array of userIds when inserting
// and index it for quick unread counts.
db.notifications.createIndex({ 'unreadRecipients': 1 }, { name: 'idx_unread_recipients' });

print('Mongo notifications collection initialized.');
