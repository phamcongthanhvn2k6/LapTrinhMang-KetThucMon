package com.qlsv.network;

public enum PacketType {
    PING,                     // Client pings Server (Heartbeat)
    PONG,                     // Server responds to Ping
    CONNECT_DB,               // Client sends SQL credentials to Server
    DB_STATUS,                // Server returns DB connection success/failure
    LOGIN,                    // Client sends Username/Password for Server authentication
    LOGIN_RESPONSE,           // Server returns auth token and user profile
    ADD_STUDENT,              // Client sends a student record
    UPDATE_STUDENT,           // Client updates an existing student record
    DELETE_STUDENT,           // Client deletes a student record
    STUDENT_RESULT,           // Server returns calculated student result
    SEARCH_STUDENTS,          // Client searches students by query/filters
    GET_ALL_STUDENTS,         // Client fetches all student records
    STUDENTS_LIST,            // Server returns array of student records
    GET_STUDENTS_PAGE,        // Client fetches a paginated slice of students
    STUDENTS_PAGE_RESULT,     // Server returns paginated PageResult
    GET_TRANSCRIPT,           // Client fetches full transcript with course grades
    TRANSCRIPT_RESULT,        // Server returns full transcript object
    UPDATE_COURSE_GRADE,      // Teacher updates grade of a specific course in transcript
    SUBMIT_GRADE_APPEAL,      // Student submits a grade appeal request to Teacher/Server
    GET_GRADE_APPEALS,        // Teacher/Student fetches list of grade appeals
    GRADE_APPEALS_RESULT,     // Server returns list of GradeAppeal objects
    RESOLVE_GRADE_APPEAL,     // Teacher approves (with new scores) or rejects a grade appeal
    REGISTER_CREDITS,         // Student submits next-semester credit registration for validation
    ROTATE_CRYPTO_KEY,        // Admin triggers live re-encryption of all DB records (DES <-> AES-256-GCM)
    DOWNLOAD_COURSE_MATERIAL, // Student requests chunked RUDP download of course syllabus/material
    COURSE_MATERIAL_CHUNK,    // Server sends a specific chunk (chunkIndex/totalChunks) + SHA-256 checksum
    UPLOAD_COURSE_MATERIAL,   // Teacher uploads/updates course syllabus material on Server
    IMPORT_EXCEL,             // Client sends list of students from Excel for bulk insert
    IMPORT_RESULT,            // Server returns number of records imported
    PUSH_SUBSCRIBE,           // Client registers async UDP push listener port
    PUSH_EVENT,               // Server pushes real-time notification/grade update to Clients
    TOGGLE_GRADE_LOCK,        // Admin locks/unlocks semester gradebook
    GRADE_LOCK_STATUS,        // Server returns current grade lock status
    BROADCAST_ANNOUNCEMENT,   // Admin/Teacher broadcasts announcement to all active clients
    GET_AUDIT_LOGS,           // Admin requests security & grade audit trail
    AUDIT_LOGS_RESULT,        // Server returns recent audit trail records
    ACK,                      // RUDP Acknowledgment packet
    ERROR                     // Server returns error message
}
