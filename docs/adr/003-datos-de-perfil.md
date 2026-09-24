# ADR-003. Los datos de perfil viven en nuestra base de datos

El token trae poco: el identificador del usuario (`sub`) y, si el proveedor lo entrega, el correo y el nombre. Pero nosotros necesitamos más cosas (teléfono, nombre de usuario, y mañana los datos de facturación).

Podríamos pedirle al proveedor que las metiera en el token como claims personalizados, o mantener nuestra tabla sincronizada con la suya. Elegimos lo tercero: **guardar todo eso solo en nuestra base de datos y que el usuario complete su perfil desde nuestra API**.

Los claims personalizados hay que asignarlos usuario por usuario contra la API del proveedor, así que tendríamos que escribir en Firebase cada vez que alguien cambia su teléfono. Eso nos ata al proveedor justo en lo que queríamos mantener intercambiable, engorda el token con datos que no sirven para autorizar nada, y crea un segundo sitio donde vive la misma información, con lo que eso implica cuando los dos dejan de coincidir.

Lo que viene en el token lo usamos solo para rellenar el usuario la primera vez. A partir de ahí, manda nuestra tabla.

La contrapartida es que un usuario recién registrado tiene el perfil a medias y no se entera hasta que intenta hacer algo que necesita esos datos, como reservar. Lo asumimos: le devolvemos un error claro diciéndole qué le falta y se lo encuentra en el momento en que le importa. A cambio, el registro es tan corto como el proveedor quiera y no le pedimos el teléfono a alguien que a lo mejor solo entra a mirar horarios.
