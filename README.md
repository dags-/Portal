# Portal
Create areas that teleport players on entry, maintaining their relative position & velocity 

| Command | Permission | Description |
| :------ | :--------- | :---------- |
| `portal pos1` | `portal.command.create` | Set the first position of the portal |
| `portal pos2` | `portal.command.create` | Set the second position of the portal |
| `portal create <name>` | `portal.command.create` | Create a portal with the given name |
| `portal delete <name>` | `portal.command.delete` | Delete a portal with the given name |
| `portal link <from> <to> (-r)` | `portal.command.link` | Link from one portal to another (-r to link in the reverse direction as well) |
| `portal unlink <portal>` | `portal.command.unlink` | Unlink any portals connected to the given portal |