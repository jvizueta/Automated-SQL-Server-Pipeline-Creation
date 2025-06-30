USE master;
GO
-- the original database (use 'SET @DB = NULL' to disable backup)
DECLARE @envName VARCHAR(50)
DECLARE @DropQuery varchar(2000)
DECLARE @BackupFile varchar(2000)
DECLARE @TargetDatabaseName varchar(200)
DECLARE @TargetDatabaseNamePrefix varchar(200)
DECLARE @TargetDatabaseFolder varchar(2000)

DECLARE @DataFileLogicalName varchar(2000)
DECLARE @DataFile varchar(2000)
DECLARE @LogFileLogicalName varchar(2000)
DECLARE @LogFile varchar(2000)
DECLARE @RestoreQuery varchar(2000)
DECLARE @FileListQuery varchar(200)


DECLARE @FileListTable TABLE (
    [LogicalName]           NVARCHAR(128),
    [PhysicalName]          NVARCHAR(260),
    [Type]                  CHAR(1),
    [FileGroupName]         NVARCHAR(128),
    [Size]                  NUMERIC(20,0),
    [MaxSize]               NUMERIC(20,0),
    [FileID]                BIGINT,
    [CreateLSN]             NUMERIC(25,0),
    [DropLSN]               NUMERIC(25,0),
    [UniqueID]              UNIQUEIDENTIFIER,
    [ReadOnlyLSN]           NUMERIC(25,0),
    [ReadWriteLSN]          NUMERIC(25,0),
    [BackupSizeInBytes]     BIGINT,
    [SourceBlockSize]       INT,
    [FileGroupID]           INT,
    [LogGroupGUID]          UNIQUEIDENTIFIER,
    [DifferentialBaseLSN]   NUMERIC(25,0),
    [DifferentialBaseGUID]  UNIQUEIDENTIFIER,
    [IsReadOnly]            BIT,
    [IsPresent]             BIT,
    [TDEThumbprint]         VARBINARY(32), -- remove this column if using SQL 2005
	[SnapshotUrl]			NVARCHAR(360) -- as per msdn.microsoft.com/en-us/library/ms173778.aspx, but I am unsure whether that is for SQL Server 2014 or 2016 (I think it starts in 2016...)
)

-- ****************************************************************
SET @BackupFile = '$(BackupFile)'                                  -- FileName of the backup file
SET @TargetDatabaseFolder = "$(TargetDBFolder)"
-- ****************************************************************

DECLARE dbEnvs_cursor CURSOR FOR
SELECT value FROM STRING_SPLIT("$(TargetDBNames)",',')

OPEN dbEnvs_cursor
FETCH NEXT FROM dbEnvs_cursor INTO @TargetDatabaseName

WHILE @@FETCH_STATUS = 0
BEGIN

	SET @TargetDatabaseName=TRIM(@TargetDatabaseName)
	PRINT @TargetDatabaseName
	-- Drop @TargetDatabaseName if exists
	IF EXISTS(SELECT * FROM sysdatabases WHERE name = @TargetDatabaseName)
	BEGIN
	SET @DropQuery = 'DROP DATABASE ' + @TargetDatabaseName
	PRINT 'Executing query : ' + @DropQuery;
	EXEC (@DropQuery)
	END
	PRINT 'OK!'

	SET @DataFile = @TargetDatabaseFolder + @TargetDatabaseName + '.mdf';
	SET @LogFile = @TargetDatabaseFolder + @TargetDatabaseName + '.ldf';

    SET @FileListQuery = 'RESTORE FILELISTONLY FROM DISK =  ' + QUOTENAME(@BackupFile ,'''')
    PRINT 'Executing query: ' + @FileListQuery
    INSERT INTO @FileListTable EXEC(@FileListQuery);
    SELECT @DataFileLogicalName = LogicalName FROM @FileListTable WHERE type = 'D';
    SELECT @LogFileLogicalName = LogicalName FROM @FileListTable WHERE type = 'L';

	-- Restore database from @BackupFile into @DataFile and @LogFile
	SET @RestoreQuery = 'RESTORE DATABASE ' + @TargetDatabaseName + ' FROM DISK = ' + QUOTENAME(@BackupFile,'''') 
	SET @RestoreQuery = @RestoreQuery + ' WITH MOVE ' + QUOTENAME(@DataFileLogicalName,'''') + ' TO ' + QUOTENAME(@DataFile ,'''')
	SET @RestoreQuery = @RestoreQuery + ' , MOVE ' + QUOTENAME(@LogFileLogicalName,'''') + ' TO ' + QUOTENAME(@LogFile,'''')
	PRINT 'Executing query : ' + @RestoreQuery
	EXEC (@RestoreQuery)
	PRINT 'OK!'

	FETCH NEXT FROM dbEnvs_cursor INTO @TargetDatabaseName

END

CLOSE dbEnvs_cursor
DEALLOCATE dbEnvs_cursor