use extism_pdk::*;
use toml_edit::{table, value, DocumentMut, Item};

#[host_fn("extism:host/user")]
extern "ExtismHost" {
    fn host_log(input: &String) -> ();
}

fn log(message: &String) -> () {
    unsafe { host_log((message).into()).unwrap() };
}

fn panic<T>(message: &String) -> T {
    log(&message);
    panic!("{}", &message);
}

#[plugin_fn]
pub fn set_toml(toml: String) -> FnResult<()> {
    match var::set("toml", toml) {
        Ok(()) => (),
        Err(error) => panic(&format!("error while set variable: {}", error)),
    }
    Ok(())
}

#[plugin_fn]
pub fn get_toml() -> FnResult<String> {
    match var::get("toml") {
        Ok(option) => match option {
            Some(toml) => Ok(toml),
            None => Ok("".to_string()),
        },
        Err(error) => panic(&format!("error while get variable: {}", error)),
    }
}

#[derive(serde::Deserialize)]
struct SetValue {
    path: Vec<String>,
    value: String,
}

#[plugin_fn]
pub fn set_value(json: Json<SetValue>) -> FnResult<()> {
    let toml: String = var::get("toml").unwrap().unwrap();
    let mut document = toml.parse::<DocumentMut>().expect("invalid doc");
    let parameter = json.into_inner();
    let mut item = document.as_item_mut();
	let path = &parameter.path;
    let last = path.last().unwrap();
    for step in path {
		// create table if not exists
		if step != last && item.get(step).is_none() {
			*item.get_mut(step).unwrap() = table();
		}
		item = item.get_mut(step).unwrap();
    }
    *item = value(parameter.value);

    match var::set("toml", document.to_string()) {
        Ok(()) => (),
        Err(error) => panic(&format!("error while set variable: {}", error)),
    }
    Ok(())
}

#[derive(serde::Deserialize)]
struct UnsetValue {
    path: Vec<String>,
}

#[plugin_fn]
pub fn unset_value(json: Json<UnsetValue>) -> FnResult<()> {
    let toml: String = var::get("toml").unwrap().unwrap();
    let mut document = toml.parse::<DocumentMut>().expect("invalid doc");
    let parameter = json.into_inner();
    let mut item = document.as_item_mut();
    for step in parameter.path {
        item = item.get_mut(step).unwrap();
    }
    *item = Item::None;
    match var::set("toml", document.to_string()) {
        Ok(()) => (),
        Err(error) => panic(&format!("error while set variable: {}", error)),
    }
    Ok(())
}
