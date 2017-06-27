package org.clapper.peoplegen

object Fields {
  object FieldSet extends Enumeration {
    type FieldSet = Value

    val ID         = Value
    val FirstName  = Value
    val MiddleName = Value
    val LastName   = Value
    val BirthDate  = Value
    val SSN        = Value
    val Gender     = Value
    val Salary     = Value

    def inOrder = Seq(
      ID, FirstName, MiddleName, LastName, Gender, BirthDate, SSN, Salary
    )
  }

  val FieldNames = Map(
    HeaderFormat.English -> Map(
      FieldSet.ID         -> "id",
      FieldSet.FirstName  -> "first name",
      FieldSet.MiddleName -> "middle name",
      FieldSet.LastName   -> "last name",
      FieldSet.BirthDate  -> "birth date",
      FieldSet.SSN        -> "ssn",
      FieldSet.Gender     -> "gender",
      FieldSet.Salary     -> "salary"
    ),
    HeaderFormat.CamelCase -> Map(
      FieldSet.ID         -> "id",
      FieldSet.FirstName  -> "firstName",
      FieldSet.MiddleName -> "middleName",
      FieldSet.LastName   -> "lastName",
      FieldSet.BirthDate  -> "birthDate",
      FieldSet.SSN        -> "ssn",
      FieldSet.Gender     -> "gender",
      FieldSet.Salary     -> "salary"
    ),
    HeaderFormat.SnakeCase -> Map(
      FieldSet.ID         -> "id",
      FieldSet.FirstName  -> "first_name",
      FieldSet.MiddleName -> "middle_name",
      FieldSet.LastName   -> "last_name",
      FieldSet.BirthDate  -> "birth_date",
      FieldSet.SSN        -> "ssn",
      FieldSet.Gender     -> "gender",
      FieldSet.Salary     -> "salary"
    )
  )
}
