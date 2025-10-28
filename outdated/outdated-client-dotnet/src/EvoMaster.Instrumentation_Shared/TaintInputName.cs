using System;
using System.Text.RegularExpressions;
using EvoMaster.Client.Util.Extensions;

namespace EvoMaster.Instrumentation_Shared {
    public class TaintInputName {
        /*
            WARNING:
            the naming here has to be kept in sync in ALL implementations of this class,
            including Java, JS and C#
         */

        private const string Prefix = "_EM_";

        private const string Postfix = "_XYZ_";

        private static readonly Regex Pattern = new Regex($"{Regex.Escape(Prefix)}\\d+{Regex.Escape(Postfix)}", RegexOptions.IgnoreCase);

        static TaintInputName(){
            
        }


        /// <summary>
        /// Check if a given string value is a tainted value
        /// </summary>
        /// <param name="value"></param>
        /// <returns></returns>
        public static bool IsTaintInput(string value) => value != null && Pattern.IsEntirelyMatch(value);


        public static bool IncludesTaintInput(string value) => value != null && Pattern.IsMatch(value);


        /**
     * Create a tainted value, with the input id being part of it
     */
        public static string GetTaintName(int id) {
            if (id < 0) {
                throw new ArgumentException("Negative id");
            }

            /*
                Note: this is quite simple, we simply add a unique prefix
                and postfix, in lowercase.
                But we would not be able to check if the part of the id was
                modified.
             */
            return Prefix + id + Postfix;
        }

        /**
     * One problem when using this type of taint, is that there can be constraints on the length
     * of the strings... and the taint value might end up being longer than it :-(
     * Not sure if there is really any simple workaround... but hopefully should be
     * so rare that we can live with it
     */
        public static int GetTaintNameMaxLength() {
            return Prefix.Length + Postfix.Length + 6;
        }
    }
}