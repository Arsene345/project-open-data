package fr.enzor.projectopendata.modele

data class Vote(
    val adresse: Any?,
    val canton_code: String?,
    val circonscription_code: String?,
    val code: String?,
    val com_arm_name: String?,
    val com_code: String?,
    val com_name: String?,
    val dep_code: String?,
    val dep_name: String?,
    val epci_code: String?,
    val epci_name: String?,
    val libelle: String?,
    val location: Location?,
    val postal_code: String?,
    val reg_code: String?,
    val reg_name: String?
){

    // ID stable genere cote appli a partir de plusieurs champs
    val id: String
        get() = listOf(
            com_code.orEmpty(),
            code.orEmpty(),
            postal_code.orEmpty()
        ).joinToString("_")

    override fun toString(): String {
        return "Vote(adresse=$adresse, canton_code='$canton_code', circonscription_code='$circonscription_code', code='$code', com_arm_name='$com_arm_name', com_code='$com_code', com_name='$com_name', dep_code='$dep_code', dep_name='$dep_name', epci_code='$epci_code', epci_name='$epci_name', libelle='$libelle', location=$location, postal_code='$postal_code', reg_code='$reg_code', reg_name='$reg_name')"
    }
}