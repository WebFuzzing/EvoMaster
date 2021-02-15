
using System;
using Controller.Api;

namespace Controller.Controllers.db
{
    public class DbUnsupportedException : InvalidOperationException
    {

        public DbUnsupportedException(DatabaseType type) : this(type.ToString())
        {
        }

        public DbUnsupportedException(string type) : base("Database type " + type + " is not supported")
        {
        }

    }
}