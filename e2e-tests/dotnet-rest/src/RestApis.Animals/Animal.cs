using System;

namespace RestApis.Animals
{
    public class Animal
    {
        public Animal(string name)
        {
            Name = name;
        }

        public int Id { get; set; }
        public string Name { get; set; }
    }
}
