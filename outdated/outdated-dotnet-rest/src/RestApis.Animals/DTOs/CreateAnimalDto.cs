using System.ComponentModel.DataAnnotations;

namespace RestApis.Animals.Dtos {
    public class CreateAnimalDto {
        [Required] public string Name { get; set; }
    }
}